/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;

/**
 * Converts from an ODRL prohibition as a {@link JsonObject} in JSON-LD expanded form to a {@link Prohibition}.
 */
public class JsonObjectToProhibitionTransformer extends AbstractJsonLdTransformer<JsonObject, Prohibition> {
    
    public JsonObjectToProhibitionTransformer() {
        super(JsonObject.class, Prohibition.class);
    }
    
    @Override
    public @Nullable Prohibition transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Prohibition.Builder.newInstance();

        visitProperties(object, key -> {
            switch (key) {
                case ODRL_ACTION_ATTRIBUTE:
                    return value -> builder.action(transformObject(value, Action.class, context));
                case ODRL_CONSTRAINT_ATTRIBUTE:
                    return value -> builder.constraints(transformArray(value, Constraint.class, context));
                case ODRL_TARGET_ATTRIBUTE:
                    return value -> builder.target(transformString(value, context));
                default:
                    return doNothing();
            }
        });

        return builderResult(builder::build, context);
    }

}
