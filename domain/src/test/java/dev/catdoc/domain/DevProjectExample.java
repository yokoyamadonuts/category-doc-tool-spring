package dev.catdoc.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 既存版の examples/dev-project/category.yaml を Java で組み立てたもの。
 * 要件、仕様、設計、内部仕様、インフラの5層を、4つの関手でつなぐ。
 */
final class DevProjectExample {

    private DevProjectExample() {
    }

    static Category requirements() {
        return new Category("requirements", "Requirements",
                List.of(
                        CategoryObject.of("req-user-management", "ユーザー管理要件", "requirements"),
                        CategoryObject.of("req-auth-system", "認証システム要件", "requirements")),
                List.of(
                        Morphism.of("req-auth-depends-user", "depends-on", "req-auth-system", "req-user-management"),
                        Morphism.of("id-req-user", "id_user", "req-user-management", "req-user-management"),
                        Morphism.of("id-req-auth", "id_auth", "req-auth-system", "req-auth-system")));
    }

    static Category specifications() {
        return new Category("specifications", "Specifications",
                List.of(
                        CategoryObject.of("spec-user-api", "ユーザーAPI仕様", "specifications"),
                        CategoryObject.of("spec-auth-api", "認証API仕様", "specifications")),
                List.of(
                        Morphism.of("spec-auth-uses-user", "uses", "spec-auth-api", "spec-user-api"),
                        Morphism.of("id-spec-user", "id_user_api", "spec-user-api", "spec-user-api"),
                        Morphism.of("id-spec-auth", "id_auth_api", "spec-auth-api", "spec-auth-api")));
    }

    static Category design() {
        return new Category("design", "Design",
                List.of(
                        CategoryObject.of("design-user-service", "ユーザーサービス設計", "design"),
                        CategoryObject.of("design-auth-service", "認証サービス設計", "design")),
                List.of(
                        Morphism.of("design-auth-calls-user", "calls", "design-auth-service", "design-user-service"),
                        Morphism.of("id-design-user", "id_user_svc", "design-user-service", "design-user-service"),
                        Morphism.of("id-design-auth", "id_auth_svc", "design-auth-service", "design-auth-service")));
    }

    static Category internalSpec() {
        return new Category("internal-spec", "InternalSpec",
                List.of(
                        CategoryObject.of("internal-user-repository", "UserRepositoryモジュール仕様", "internal-spec"),
                        CategoryObject.of("internal-jwt-handler", "JWTHandlerモジュール仕様", "internal-spec")),
                List.of(
                        Morphism.of("internal-jwt-uses-user", "imports", "internal-jwt-handler", "internal-user-repository"),
                        Morphism.of("id-internal-user-repo", "id_user_repo", "internal-user-repository", "internal-user-repository"),
                        Morphism.of("id-internal-jwt", "id_jwt", "internal-jwt-handler", "internal-jwt-handler")));
    }

    static Category infrastructure() {
        return new Category("infrastructure", "Infrastructure",
                List.of(
                        CategoryObject.of("infra-k8s-user", "ユーザーサービスKubernetes設定", "infrastructure"),
                        CategoryObject.of("infra-k8s-auth", "認証サービスKubernetes設定", "infrastructure"),
                        CategoryObject.of("infra-database", "PostgreSQLデータベース", "infrastructure")),
                List.of(
                        Morphism.of("infra-user-uses-db", "connects", "infra-k8s-user", "infra-database"),
                        Morphism.of("infra-auth-uses-db", "connects", "infra-k8s-auth", "infra-database"),
                        Morphism.of("infra-auth-uses-user", "depends", "infra-k8s-auth", "infra-k8s-user"),
                        Morphism.of("id-infra-user", "id_k8s_user", "infra-k8s-user", "infra-k8s-user"),
                        Morphism.of("id-infra-auth", "id_k8s_auth", "infra-k8s-auth", "infra-k8s-auth"),
                        Morphism.of("id-infra-db", "id_db", "infra-database", "infra-database")));
    }

    static List<Category> categories() {
        return List.of(requirements(), specifications(), design(), internalSpec(), infrastructure());
    }

    static Functor specify() {
        return new Functor("specify-functor", "SpecifyFunctor (要件→仕様)", "requirements", "specifications",
                ordered("req-user-management", "spec-user-api", "req-auth-system", "spec-auth-api"),
                ordered("req-auth-depends-user", "spec-auth-uses-user",
                        "id-req-user", "id-spec-user", "id-req-auth", "id-spec-auth"));
    }

    static Functor designFunctor() {
        return new Functor("design-functor", "DesignFunctor (仕様→設計)", "specifications", "design",
                ordered("spec-user-api", "design-user-service", "spec-auth-api", "design-auth-service"),
                ordered("spec-auth-uses-user", "design-auth-calls-user",
                        "id-spec-user", "id-design-user", "id-spec-auth", "id-design-auth"));
    }

    static Functor implement() {
        return new Functor("implement-functor", "ImplementFunctor (設計→内部仕様)", "design", "internal-spec",
                ordered("design-user-service", "internal-user-repository", "design-auth-service", "internal-jwt-handler"),
                ordered("design-auth-calls-user", "internal-jwt-uses-user",
                        "id-design-user", "id-internal-user-repo", "id-design-auth", "id-internal-jwt"));
    }

    static Functor deploy() {
        return new Functor("deploy-functor", "DeployFunctor (内部仕様→インフラ)", "internal-spec", "infrastructure",
                ordered("internal-user-repository", "infra-k8s-user", "internal-jwt-handler", "infra-k8s-auth"),
                ordered("internal-jwt-uses-user", "infra-auth-uses-user",
                        "id-internal-user-repo", "id-infra-user", "id-internal-jwt", "id-infra-auth"));
    }

    static List<Functor> functors() {
        return List.of(specify(), designFunctor(), implement(), deploy());
    }

    /** キーと値を交互に並べた引数から、順序を保った Map を作る。 */
    static Map<String, String> ordered(String... keyValues) {
        var map = new LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
