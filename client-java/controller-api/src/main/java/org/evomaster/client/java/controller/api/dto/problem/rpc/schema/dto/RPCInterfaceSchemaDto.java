package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto;

import java.util.List;

/**
 * created by manzhang on 2021/11/27
 */
public class RPCInterfaceSchemaDto {

    public String interfaceId;

    public List<RPCActionDto> endpoints;

    public List<TypeDto> types;

}
