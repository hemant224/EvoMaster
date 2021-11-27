package org.evomaster.client.java.controller.api.dto.problem.rpc.schema;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.RPCInterfaceSchemaDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.TypeSchema;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * schema dto of the RCP service
 */
public final class InterfaceSchema{
    /**
     * name of the interface
     */
    private final String name;

    /**
     * a list of available endpoints in the service
     */
    private List<EndpointSchema> endpoints;

    /**
     * key is the full name of type
     * value is one example
     */
    private Map<String, TypeSchema> typeCollections = new HashMap<>();


    public InterfaceSchema(String name, List<EndpointSchema> endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    public void registerType(TypeSchema type){
        String typeName = type.getFullTypeName();
        if (!typeCollections.containsKey(typeName))
            typeCollections.put(typeName, type.copy());
    }

    public TypeSchema getTypeOrNull(String name){
        return typeCollections.get(name);
    }

    public List<EndpointSchema> getEndpoints(){
        return endpoints;
    }

    public List<EndpointSchema> findEndpoints(String name){
        return endpoints.stream().filter(s-> s.getName().equals(name)).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public Map<String, TypeSchema> getTypeCollections() {
        return typeCollections;
    }

    public RPCInterfaceSchemaDto getDto(){
        RPCInterfaceSchemaDto dto = new RPCInterfaceSchemaDto();
        dto.interfaceId = this.getName();
        dto.types = typeCollections.values().stream().map(TypeSchema::getDto).collect(Collectors.toList());
        dto.endpoints = endpoints.stream().map(EndpointSchema::getDto).collect(Collectors.toList());
        return dto;
    }
}
