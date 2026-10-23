package dev.catdoc.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 圏、関手、自然変換の公理を確かめる。誤りの文言は既存版に合わせる。 */
public final class VerificationService {

    private VerificationService() {
    }

    /** 恒等射の存在、射の参照の整合性、合成の閉包を確かめる。合成の欠けは警告にとどめる。 */
    public static VerificationResult verifyCategory(Category category) {
        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();
        var morphisms = category.morphisms();

        for (var object : category.objects()) {
            var id = object.id();
            if (morphisms.stream().noneMatch(m -> m.source().equals(id) && m.target().equals(id))) {
                errors.add("Object '%s' lacks an identity morphism (id: %s → %s)".formatted(id, id, id));
            }
        }

        Set<String> objectIds = category.objects().stream().map(CategoryObject::id).collect(Collectors.toSet());
        for (var m : morphisms) {
            if (!objectIds.contains(m.source())) {
                errors.add("Morphism '%s' has undefined source object '%s'".formatted(m.id(), m.source()));
            }
            if (!objectIds.contains(m.target())) {
                errors.add("Morphism '%s' has undefined target object '%s'".formatted(m.id(), m.target()));
            }
        }

        for (var f : morphisms) {
            for (var g : morphisms) {
                if (f.isIdentity() || g.isIdentity() || !f.target().equals(g.source())) {
                    continue;
                }
                boolean composed = morphisms.stream()
                        .anyMatch(m -> m.source().equals(f.source()) && m.target().equals(g.target()));
                if (!composed) {
                    warnings.add("Missing composition: %s∘%s (%s → %s)"
                            .formatted(g.name(), f.name(), f.source(), g.target()));
                }
            }
        }

        return new VerificationResult(errors, warnings);
    }

    /**
     * 文書の対応の完全性、対応先の存在、恒等射の保存、射の始点と終点の保存を確かめる。
     * 恒等射は ID の命名ではなく、始点と終点が同じ射として探す。
     */
    public static VerificationResult verifyFunctor(Functor functor, Category source, Category target) {
        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        for (var object : source.objects()) {
            if (functor.mapObject(object.id()).isEmpty()) {
                errors.add("Object '%s' is not mapped by functor '%s'".formatted(object.id(), functor.id()));
            }
        }

        functor.objectMapping().forEach((from, to) -> {
            if (target.object(to).isEmpty()) {
                errors.add("Functor '%s' maps '%s' to non-existent object '%s'".formatted(functor.id(), from, to));
            }
        });

        for (var identity : source.morphisms()) {
            if (!identity.isIdentity()) {
                continue;
            }
            var objectId = identity.source();
            var mappedObject = functor.mapObject(objectId);
            var mappedMorphism = functor.mapMorphism(identity.id());
            if (mappedObject.isEmpty() || mappedMorphism.isEmpty()) {
                continue;
            }
            boolean preserved = target.morphism(mappedMorphism.get())
                    .filter(m -> m.source().equals(mappedObject.get()) && m.target().equals(mappedObject.get()))
                    .isPresent();
            if (!preserved) {
                errors.add("Functor '%s' does not preserve identity for object '%s': F(%s) = %s is not the identity on '%s'"
                        .formatted(functor.id(), objectId, identity.id(), mappedMorphism.get(), mappedObject.get()));
            }
        }

        checkMorphismPreservation(functor, source, target, errors, warnings);

        return new VerificationResult(errors, warnings);
    }

    /**
     * 射の対応が始点と終点を保つか確かめる。f: A → B なら F(f): F(A) → F(B) でなければならない。
     * 向きが違えば errors に足す。対応がない、または対応先の射がないときは warnings に足す。
     * 恒等射は上の恒等射の検査に任せる。
     */
    static void checkMorphismPreservation(Functor functor, Category source, Category target,
            List<String> errors, List<String> warnings) {
        for (var f : source.morphisms()) {
            if (f.isIdentity()) {
                continue;
            }

            var mappedId = functor.mapMorphism(f.id());
            if (mappedId.isEmpty()) {
                warnings.add("Morphism '%s' is not mapped by functor '%s'".formatted(f.id(), functor.id()));
                continue;
            }

            var image = target.morphism(mappedId.get());
            if (image.isEmpty()) {
                warnings.add("Functor '%s' maps morphism '%s' to non-existent morphism '%s'"
                        .formatted(functor.id(), f.id(), mappedId.get()));
                continue;
            }

            // 文書の対応の誤りは上の検査が出しているので、ここでは重ねて出さない
            var expectedSource = functor.mapObject(f.source());
            var expectedTarget = functor.mapObject(f.target());
            if (expectedSource.isEmpty() || expectedTarget.isEmpty()) {
                continue;
            }

            var g = image.get();
            if (!g.source().equals(expectedSource.get()) || !g.target().equals(expectedTarget.get())) {
                errors.add("Functor '%s' does not preserve morphism '%s': F(%s) = %s goes %s → %s, expected %s → %s"
                        .formatted(functor.id(), f.id(), f.id(), g.id(), g.source(), g.target(),
                                expectedSource.get(), expectedTarget.get()));
            }
        }
    }

    /**
     * F と G が同じ元と先のカテゴリを持ち、元のカテゴリの各文書 A について、
     * 成分 η_A が先のカテゴリの F(A) から G(A) への射であることを確かめる。
     */
    public static VerificationResult verifyNaturalTransformation(NaturalTransformation nt, Functor sourceFunctor,
            Functor targetFunctor, Category source, Category target) {
        var errors = new ArrayList<String>();

        if (!sourceFunctor.sourceCategory().equals(targetFunctor.sourceCategory())
                || !sourceFunctor.targetCategory().equals(targetFunctor.targetCategory())) {
            errors.add("Natural transformation '%s' connects functors '%s' (%s → %s) and '%s' (%s → %s) with different categories"
                    .formatted(nt.id(), sourceFunctor.id(), sourceFunctor.sourceCategory(), sourceFunctor.targetCategory(),
                            targetFunctor.id(), targetFunctor.sourceCategory(), targetFunctor.targetCategory()));
            return new VerificationResult(errors, List.of());
        }

        for (var object : source.objects()) {
            var id = object.id();
            var component = nt.component(id);
            if (component.isEmpty()) {
                errors.add("Natural transformation '%s' is missing component for object '%s'".formatted(nt.id(), id));
                continue;
            }
            var morphism = target.morphism(component.get());
            if (morphism.isEmpty()) {
                errors.add("Natural transformation '%s' has non-existent component morphism '%s' for object '%s'"
                        .formatted(nt.id(), component.get(), id));
                continue;
            }
            var expectedSource = sourceFunctor.mapObject(id);
            var expectedTarget = targetFunctor.mapObject(id);
            if (expectedSource.isEmpty() || expectedTarget.isEmpty()) {
                continue;
            }
            var m = morphism.get();
            if (!m.source().equals(expectedSource.get())) {
                errors.add("Component η_%s has wrong source: expected '%s', got '%s'"
                        .formatted(id, expectedSource.get(), m.source()));
            }
            if (!m.target().equals(expectedTarget.get())) {
                errors.add("Component η_%s has wrong target: expected '%s', got '%s'"
                        .formatted(id, expectedTarget.get(), m.target()));
            }
        }

        return new VerificationResult(errors, List.of());
    }
}
