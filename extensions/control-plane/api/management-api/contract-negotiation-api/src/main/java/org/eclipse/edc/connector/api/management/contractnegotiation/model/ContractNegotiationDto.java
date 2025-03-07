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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.MutableDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = ContractNegotiationDto.Builder.class)
public class ContractNegotiationDto extends MutableDto {
    // constants used for JSON-LD transformation
    public static final String CONTRACT_NEGOTIATION_TYPE = EDC_NAMESPACE + "ContractNegotiationDto";
    public static final String CONTRACT_NEGOTIATION_AGREEMENT_ID = EDC_NAMESPACE + "contractAgreementId";
    public static final String CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR = EDC_NAMESPACE + "counterPartyAddress";
    public static final String CONTRACT_NEGOTIATION_ERRORDETAIL = EDC_NAMESPACE + "errorDetail";
    public static final String CONTRACT_NEGOTIATION_PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String CONTRACT_NEGOTIATION_STATE = EDC_NAMESPACE + "state";
    public static final String CONTRACT_NEGOTIATION_NEG_TYPE = EDC_NAMESPACE + "type";
    public static final String CONTRACT_NEGOTIATION_CALLBACK_ADDR = EDC_NAMESPACE + "callbackAddresses";

    private String contractAgreementId; // is null until state == CONFIRMED
    private String counterPartyAddress;
    private String errorDetail;

    private String protocol = "ids-multipart";
    private String state;
    private Type type = Type.CONSUMER;

    private List<CallbackAddress> callbackAddresses = new ArrayList<>();


    private ContractNegotiationDto() {
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public Type getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public String getContractAgreementId() {
        return contractAgreementId;
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends MutableDto.Builder<ContractNegotiationDto, Builder> {

        private Builder() {
            super(new ContractNegotiationDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            dto.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder state(String state) {
            dto.state = state;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            dto.errorDetail = errorDetail;
            return this;
        }

        public Builder contractAgreementId(String contractAgreementId) {
            dto.contractAgreementId = contractAgreementId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            dto.callbackAddresses = callbackAddresses;
            return this;
        }


        public Builder type(Type type) {
            dto.type = type;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
