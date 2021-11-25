package org.evomaster.client.java.controller.problem.rpc;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.*;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.*;
import org.evomaster.client.java.controller.problem.RPCType;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * created by manzhang on 2021/11/4
 */
public class RPCEndpointsBuilder {

    /**
     * @param interfaceName the name of interface
     * @param rpcType          is the type of RPC, e.g., gRPC, Thrift
     * @return an interface schema for evomaster to access
     */
    public static InterfaceSchema build(String interfaceName, RPCType rpcType) {
        List<EndpointSchema> endpoints = new ArrayList<>();
        try {
            Class<?> interfaze = Class.forName(interfaceName);
            InterfaceSchema schema = new InterfaceSchema(interfaceName, endpoints);
            for (Method m : interfaze.getDeclaredMethods()) {
                endpoints.add(build(schema, m, rpcType));
            }
            return schema;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find the interface with the name (" + interfaceName + ") and the error message is " + e.getMessage());
        }
    }

    private static EndpointSchema build(InterfaceSchema schema, Method method, RPCType rpcType) {
        List<NamedTypedValue> requestParams = new ArrayList<>();

        for (Parameter p : method.getParameters()) {
            requestParams.add(buildInputParameter(schema, p, rpcType));
        }
        NamedTypedValue response = null;
        if (!method.getReturnType().equals(Void.TYPE)) {
            response = build(schema, method.getReturnType(), method.getGenericReturnType(), null, rpcType, new ArrayList<>());
        }

        return new EndpointSchema(method.getName(), requestParams, response);
    }

    private static NamedTypedValue buildInputParameter(InterfaceSchema schema, Parameter parameter, RPCType type) {
        String name = parameter.getName();
        Class<?> clazz = parameter.getType();
        List<String> depth = new ArrayList<>();
        return build(schema, clazz, parameter.getParameterizedType(), name, type, depth);
    }

    private static NamedTypedValue build(InterfaceSchema schema, Class<?> clazz, Type genericType, String name, RPCType rpcType, List<String> depth) {

        depth.add(clazz.getName());
        NamedTypedValue namedValue = null;

        if (PrimitiveOrWrapperType.isPrimitiveOrTypes(clazz)) {
            namedValue = PrimitiveOrWrapperParam.build(name, clazz);
        } else if (clazz == String.class) {
            namedValue = new StringParam(name);
        } else if (clazz.isEnum()) {
            String [] items = Arrays.stream(clazz.getEnumConstants()).map(Object::toString).toArray(String[]::new);
            EnumType enumType = new EnumType(clazz.getSimpleName(), clazz.getName(), items);
            EnumParam param = new EnumParam(name, enumType);
            //register this type in the schema
            schema.registerType(enumType.copy());
            namedValue = param;
        } else if (clazz.isArray()){
            if (rpcType == RPCType.THRIFT)
                throw new RuntimeException("Array should not exist in Thrift service");
            if (!(genericType instanceof  GenericArrayType)){
                throw new RuntimeException("genericType is "+ genericType.getTypeName());
            }
            Type type = ((GenericArrayType)genericType).getGenericComponentType();
            Class<?> templateClazz = getTemplateClass(type);
            NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth);
            CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template);
            namedValue = new ArrayParam(name, ctype);
        } else if (clazz == ByteBuffer.class){
            // handle binary of thrift
            namedValue = new ByteBufferParam(name);
        } else if (clazz.isAssignableFrom(List.class) || clazz.isAssignableFrom(Set.class)){
            if (genericType == null)
                throw new RuntimeException("genericType should not be null for List and Set class");
            Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            Class<?> templateClazz = getTemplateClass(type);
            NamedTypedValue template = build(schema, templateClazz, type,"template", rpcType, depth);
            CollectionType ctype = new CollectionType(clazz.getSimpleName(),clazz.getName(), template);
            if (clazz.isAssignableFrom(List.class))
                namedValue = new ListParam(name, ctype);
            else
                namedValue = new SetParam(name, ctype);
        } else if (clazz.isAssignableFrom(Map.class)){
            if (genericType == null)
                throw new RuntimeException("genericType should not be null for List and Set class");
            Type keyType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];

            Class<?> keyTemplateClazz = getTemplateClass(keyType);
            NamedTypedValue keyTemplate = build(schema, keyTemplateClazz, keyType,"keyTemplate", rpcType, depth);

            Class<?> valueTemplateClazz = getTemplateClass(valueType);
            NamedTypedValue valueTemplate = build(schema, valueTemplateClazz, valueType,"valueTemplate", rpcType, depth);
            MapType mtype = new MapType(clazz.getSimpleName(), clazz.getName(), keyTemplate, valueTemplate);
            namedValue = new MapParam(name, mtype);
        } else {

            long cycleSize = depth.stream().filter(s-> s.equals(clazz.getName())).count();

            if (cycleSize < 3){
                List<NamedTypedValue> fields = new ArrayList<>();
                for(Field f: clazz.getDeclaredFields()){
                    NamedTypedValue field = build(schema, f.getType(), f.getGenericType(),f.getName(), rpcType, depth);
                    fields.add(field);
                }
                ObjectType otype = new ObjectType(clazz.getSimpleName(), clazz.getName(), fields);
                schema.registerType(otype);
                namedValue = new ObjectParam(name, otype);
            }else {
                CycleObjectType otype = new CycleObjectType(clazz.getSimpleName(), clazz.getName());
                namedValue = new ObjectParam(name, otype);
            }
        }


        return namedValue;
    }

    private static Class<?> getTemplateClass(Type type){
        if (type instanceof ParameterizedType){
            return  (Class<?>) ((ParameterizedType)type).getRawType();
        }else if (type instanceof Class)
            return  (Class<?>) type;
        throw new RuntimeException("unhanded type:"+ type);
    }
}
