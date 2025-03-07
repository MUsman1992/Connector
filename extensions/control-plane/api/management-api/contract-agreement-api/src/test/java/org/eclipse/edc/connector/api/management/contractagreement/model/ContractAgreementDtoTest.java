/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.contractagreement.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAgreementDtoTest {

    @Test
    void verifySerialization() throws JsonProcessingException {
        var om = new TypeManager().getMapper();
        var dto = ContractAgreementDto.Builder.newInstance()
                .assetId("test-asset-id")
                .id("test-id")
                .contractSigningDate(5432L)
                .providerId("provider")
                .consumerId("consumer")
                .policy(Policy.Builder.newInstance().build())
                .build();

        var json = om.writeValueAsString(dto);
        assertThat(json).isNotNull();

        var deserialized = om.readValue(json, ContractAgreementDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);

    }

}
