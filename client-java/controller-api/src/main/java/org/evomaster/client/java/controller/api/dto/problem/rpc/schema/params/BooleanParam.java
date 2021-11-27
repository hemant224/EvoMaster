package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * boolean param
 */
public class BooleanParam extends PrimitiveOrWrapperParam<Boolean> {
    public BooleanParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public BooleanParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.BOOLEAN;
        else
            dto.type.type = RPCSupportedDataType.P_BOOLEAN;
        return dto;
    }
}
