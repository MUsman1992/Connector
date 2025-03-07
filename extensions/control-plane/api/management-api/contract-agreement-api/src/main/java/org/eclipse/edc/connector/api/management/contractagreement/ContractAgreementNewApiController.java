/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.contractagreement;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractagreement.model.ContractAgreementDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/contractagreements")
public class ContractAgreementNewApiController implements ContractAgreementNewApi {
    private final ContractAgreementService service;
    private final JsonLd jsonLdService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public ContractAgreementNewApiController(ContractAgreementService service, JsonLd jsonLdService, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.service = service;
        this.jsonLdService = jsonLdService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }


    @POST
    @Path("/request")
    @Override
    public List<JsonObject> queryAllAgreements(JsonObject querySpecDto) {

        Function<Result<JsonObject>, Result<QuerySpec>> expandedMapper =
                expandedResult -> expandedResult
                        .compose(jsonObject -> transformerRegistry.transform(jsonObject, QuerySpecDto.class))
                        .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class));

        var query = ofNullable(querySpecDto)
                .map(jsonLdService::expand)
                .map(expandedMapper)
                .orElse(Result.success(QuerySpec.Builder.newInstance().build()))
                .orElseThrow(InvalidRequestException::new);

        try (var stream = service.query(query).orElseThrow(exceptionMapper(ContractDefinition.class, null))) {
            return stream
                    .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class)
                            .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                            .compose(jsonLdService::compact))
                    .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAgreementById(@PathParam("id") String id) {
        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class)
                        .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                        .compose(jsonLdService::compact)
                        .orElseThrow(failure -> new EdcException(failure.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(ContractAgreement.class, id));
    }


}
