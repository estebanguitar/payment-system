package com.example.paymentsystem.architecture.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 도메인 우선 패키지와 핵심 계층 의존 규칙의 회귀를 차단한다. */
class PackageArchitectureTest {
    private static JavaClasses classes;

    /** 운영 코드 전체를 한 번 읽어 각 아키텍처 규칙에서 재사용한다. */
    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter().importPackages("com.example.paymentsystem");
    }

    /** 구 역할 우선 최상위 패키지에 운영 코드가 다시 생성되지 않는지 검증한다. */
    @Test
    void legacyLayerFirstPackagesMustRemainEmpty() {
        ArchRule rule = ArchRuleDefinition.noClasses().should().resideInAnyPackage(
                "com.example.paymentsystem.application..",
                "com.example.paymentsystem.domain..",
                "com.example.paymentsystem.infrastructure..",
                "com.example.paymentsystem.presentation..");
        rule.check(classes);
    }

    /** 모든 도메인 계층이 상위 계층에 의존하지 않는지 검증한다. */
    @Test
    void domainMustNotDependOnUpperLayers() {
        ArchRule rule = ArchRuleDefinition.noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application..", "..infrastructure..", "..presentation..");
        rule.check(classes);
    }

    /** Controller가 Repository 구현에 직접 의존하지 않는지 검증한다. */
    @Test
    void controllersMustNotDependOnRepositories() {
        ArchRule rule = ArchRuleDefinition.noClasses().that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.repository..");
        rule.check(classes);
    }
}
