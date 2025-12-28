package org.iro.aiqo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RepositoryLoggingAspect {

    @Pointcut("execution(* org.iro.aiqo.api..*Repository.*(..))")
    void apiRepositories() {
    }

    @Around("apiRepositories()")
    public Object logRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodSignature = joinPoint.getSignature().toShortString();
        log.info("{} invoked with args={}", methodSignature, formatArgs(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.info("{} returned {}", methodSignature, result);
            return result;
        } catch (Throwable throwable) {
            log.error("{} threw exception", methodSignature, throwable);
            throw throwable;
        }
    }

    private String formatArgs(Object[] args) {
        return args == null ? "[]" : Arrays.toString(args);
    }
}
