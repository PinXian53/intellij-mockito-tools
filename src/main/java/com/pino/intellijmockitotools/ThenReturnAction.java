package com.pino.intellijmockitotools;

import java.util.List;
import java.util.stream.Collectors;

public class ThenReturnAction extends BaseAction{
    @Override
    String generateCode(String className, String methodName, List<String> parameterTypeList, String returnType) {
        return "when(%s.%s(%s)).thenReturn(%s);".formatted(
                className.substring(0, 1).toLowerCase() + className.substring(1),
                methodName,
                parameterTypeList.stream().map(this::toMockType).collect(Collectors.joining(", ")),
                super.toMockValue(returnType)
        );
    }
}
