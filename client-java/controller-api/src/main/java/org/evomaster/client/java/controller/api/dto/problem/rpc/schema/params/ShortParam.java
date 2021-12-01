package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * int param
 */
public class ShortParam extends PrimitiveOrWrapperParam<Short> {

    public ShortParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public ShortParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getType().isWrapper)
            dto.type.type = RPCSupportedDataType.SHORT;
        else
            dto.type.type = RPCSupportedDataType.P_SHORT;
        return dto;
    }

    @Override
    public ShortParam copyStructure() {
        return new ShortParam(getName(), getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        try {
            setValue(Short.parseShort(dto.jsonValue));
        }catch (NumberFormatException e){
            throw new RuntimeException("ERROR: fail to convert "+dto.jsonValue+" as short value");
        }

    }
}
