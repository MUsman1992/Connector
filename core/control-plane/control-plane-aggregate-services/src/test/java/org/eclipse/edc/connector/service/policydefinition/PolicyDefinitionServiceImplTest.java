/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.service.policydefinition;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PolicyDefinitionServiceImplTest {

    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore contractDefinitionStore = mock(ContractDefinitionStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final PolicyDefinitionObservable observable = mock(PolicyDefinitionObservable.class);
    private final PolicyDefinitionServiceImpl policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, observable);


    @Test
    void findById_shouldRelyOnPolicyStore() {
        when(policyStore.findById("policyId")).thenReturn(createPolicy("policyId"));

        var policy = policyServiceImpl.findById("policyId");

        assertThat(policy).isEqualTo(createPolicy("policyId"));
    }

    @Test
    void query_shouldRelyOnPolicyStore() {
        var policy = createPolicy("policyId");
        when(policyStore.findAll(any(QuerySpec.class))).thenReturn(Stream.of(policy));
        var policies = policyServiceImpl.query(QuerySpec.none());

        assertThat(policies.succeeded()).isTrue();
        assertThat(policies.getContent()).containsExactly(policy);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy.permissions.action.constraint.noexist=someval", //wrong property
            "permissions.action.constraint.leftExpression=someval", //missing root
            "policy.permissions.action.leftExpression=null" //skips path element
    })
    void query_invalidExpression_raiseException(String invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();

        assertThat(policyServiceImpl.query(query).failed()).isTrue();
    }

    @Test
    void createPolicy_shouldCreatePolicyIfItDoesNotAlreadyExist() {
        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.success(policy));

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).isEqualTo(policy);
        verify(policyStore).create(policy);
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void createPolicy_shouldNotCreatePolicyIfItAlreadyExists() {
        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.alreadyExists("test"));

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isFalse();
        verify(policyStore).create(policy);
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void delete_shouldDeletePolicyIfItsNotReferencedByAnyContractDefinition() {
        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.empty(), Stream.empty());
        when(policyStore.findById(any())).thenReturn(createPolicy("policyId"));
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(createPolicy("policyId")));

        var deleted = policyServiceImpl.deleteById("policyId");

        var result = deleted.getContent().getUid();

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("policyId"));
    }

    @Test
    void delete_shouldNotDelete_whenPolicyPartOfContractDef() {
        var policy = createPolicy("policyId");
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(policy));

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getUid())
                .contractPolicyId(policy.getUid())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "op", "right").build())
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldNotDelete_whenPolicyIsPartOfContractDefinition() {
        var policy = createPolicy("policyId");
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(policy));

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getUid())
                .contractPolicyId(policy.getUid())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "op", "right").build())
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldFailIfPolicyDoesNotExist() {
        when(policyStore.delete("policyId")).thenReturn(StoreResult.notFound("test"));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_verifyCorrectQueries() {
        var policyId = "test-policy";
        when(policyStore.delete(policyId)).thenReturn(StoreResult.success());
        policyServiceImpl.deleteById(policyId);

        verify(policyStore).delete(eq(policyId));
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void updatePolicy_ifPolicyNotExists() {
        var policy = createPolicy("policyId");
        when(policyStore.update(policy)).thenReturn(StoreResult.notFound("test"));
        var updated = policyServiceImpl.update(policy);
        assertThat(updated.succeeded()).isFalse();
        assertThat(updated.getContent()).isNull();
        verify(policyStore).update(any());
        verifyNoMoreInteractions(policyStore);

    }

    @Test
    void updatePolicy_shouldUpdateWhenExists() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);
        when(policyStore.update(policy)).thenReturn(StoreResult.success(policy));

        var updated = policyServiceImpl.update(policy);

        assertThat(updated.succeeded()).isTrue();
        verify(policyStore).update(eq(policy));
        verifyNoMoreInteractions(policyStore);
        verify(observable).invokeForEach(any());
    }

    @Test
    void updatePolicy_shouldReturnNotFound_whenNotExists() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);
        when(policyStore.update(policy)).thenReturn(StoreResult.notFound("test"));

        var updated = policyServiceImpl.update(policy);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(policyStore).update(policy);
        verifyNoMoreInteractions(policyStore);
        verify(observable, never()).invokeForEach(any());
    }

    @NotNull
    private Predicate<PolicyDefinition> hasId(String policyId) {
        return it -> policyId.equals(it.getUid());
    }

    private PolicyDefinition createPolicy(String policyId) {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id(policyId).build();
    }
}
