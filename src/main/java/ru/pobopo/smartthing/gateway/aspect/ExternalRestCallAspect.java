package ru.pobopo.smartthing.gateway.aspect;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.pobopo.smartthing.gateway.annotation.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.exception.ForbiddenCloudEndpointException;

import java.lang.reflect.Method;

@Aspect
@Slf4j
@Component
public class ExternalRestCallAspect {
    @Pointcut("within(ru.pobopo.smartthing.gateway.controller.*)")
    public void inPackage(){}

    @Before("inPackage()")
    public void accessCheck(final JoinPoint joinPoint) {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return;
        }
        String cloudRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getHeader("smt-cloud-request");
        if (StringUtils.equals(cloudRequest, "true")) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Class<?> targetClass = signature.getDeclaringType();
            Method targetMethod = signature.getMethod();
            if (!targetMethod.isAnnotationPresent(AcceptCloudRequest.class) && !targetClass.isAnnotationPresent(AcceptCloudRequest.class)) {
                log.info("Forbidden endpoint for cloud call");
                throw new ForbiddenCloudEndpointException();
            }
        }
    }
}
