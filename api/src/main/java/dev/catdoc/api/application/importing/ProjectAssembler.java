package dev.catdoc.api.application.importing;

import dev.catdoc.api.application.ImportRejectedException;
import dev.catdoc.api.application.ProjectGraph;
import dev.catdoc.domain.Category;
import dev.catdoc.domain.CategoryObject;
import dev.catdoc.domain.Functor;
import dev.catdoc.domain.Morphism;
import dev.catdoc.domain.NaturalTransformation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定義を確かめて、ドメインのモデルにする。
 *
 * <p>確かめるのは、保存と参照に要る構造だけ。必須の項目、ID の重複、存在しないカテゴリや関手への参照、
 * 文書に結びつかない Markdown を弾く。射の始点が存在しない、といった圏論の公理の違反は弾かず、検証に任せる。
 */
public final class ProjectAssembler {

    private ProjectAssembler() {
    }

    /** @throws ImportRejectedException 誤りが1つでもあれば、すべてを並べて投げる */
    public static ProjectGraph assemble(ProjectDefinition definition) {
        var errors = new ArrayList<String>();
        var documents = attachDocuments(definition);
        var contents = documents.byObject();

        var categories = new ArrayList<Category>();
        var categoryIds = new HashSet<String>();
        var objectIds = new HashSet<String>();
        for (int i = 0; i < definition.categories().size(); i++) {
            var category = definition.categories().get(i);
            var at = "categories[%d]".formatted(i);
            if (category == null) {
                errors.add(at + ": category is empty");
                continue;
            }
            boolean valid = require(errors, at, "id", category.id()) & require(errors, at, "name", category.name());
            if (category.id() != null && !categoryIds.add(category.id())) {
                errors.add("Duplicate category id '%s'".formatted(category.id()));
            }
            var objects = objects(category, at, objectIds, contents, errors);
            var morphisms = morphisms(category, at, errors);
            if (valid) {
                categories.add(new Category(category.id(), category.name(), objects, morphisms));
            }
        }

        var functors = new ArrayList<Functor>();
        var functorIds = new HashSet<String>();
        for (int i = 0; i < definition.functors().size(); i++) {
            var functor = definition.functors().get(i);
            var at = "functors[%d]".formatted(i);
            if (functor == null) {
                errors.add(at + ": functor is empty");
                continue;
            }
            boolean valid = require(errors, at, "id", functor.id())
                    & require(errors, at, "name", functor.name())
                    & require(errors, at, "sourceCategory", functor.sourceCategory())
                    & require(errors, at, "targetCategory", functor.targetCategory());
            if (functor.id() != null && !functorIds.add(functor.id())) {
                errors.add("Duplicate functor id '%s'".formatted(functor.id()));
            }
            if (!valid) {
                continue;
            }
            refer(errors, categoryIds, functor.sourceCategory(),
                    "Functor '%s' refers to unknown source category '%s'".formatted(functor.id(), functor.sourceCategory()));
            refer(errors, categoryIds, functor.targetCategory(),
                    "Functor '%s' refers to unknown target category '%s'".formatted(functor.id(), functor.targetCategory()));
            functors.add(new Functor(functor.id(), functor.name(), functor.sourceCategory(), functor.targetCategory(),
                    functor.objectMapping(), functor.morphismMapping()));
        }

        var naturalTransformations = new ArrayList<NaturalTransformation>();
        var naturalTransformationIds = new HashSet<String>();
        for (int i = 0; i < definition.naturalTransformations().size(); i++) {
            var nt = definition.naturalTransformations().get(i);
            var at = "naturalTransformations[%d]".formatted(i);
            if (nt == null) {
                errors.add(at + ": natural transformation is empty");
                continue;
            }
            boolean valid = require(errors, at, "id", nt.id())
                    & require(errors, at, "name", nt.name())
                    & require(errors, at, "sourceFunctor", nt.sourceFunctor())
                    & require(errors, at, "targetFunctor", nt.targetFunctor());
            if (nt.id() != null && !naturalTransformationIds.add(nt.id())) {
                errors.add("Duplicate natural transformation id '%s'".formatted(nt.id()));
            }
            if (!valid) {
                continue;
            }
            refer(errors, functorIds, nt.sourceFunctor(),
                    "Natural transformation '%s' refers to unknown source functor '%s'".formatted(nt.id(), nt.sourceFunctor()));
            refer(errors, functorIds, nt.targetFunctor(),
                    "Natural transformation '%s' refers to unknown target functor '%s'".formatted(nt.id(), nt.targetFunctor()));
            naturalTransformations.add(new NaturalTransformation(nt.id(), nt.name(), nt.sourceFunctor(),
                    nt.targetFunctor(), nt.components()));
        }

        // 文書の誤りは、カテゴリや関手の誤りの後に並べる
        errors.addAll(documents.duplicates());
        for (var objectId : contents.keySet()) {
            if (!objectIds.contains(objectId)) {
                errors.add("'%s' refers to unknown object '%s'".formatted(contents.get(objectId).path(), objectId));
            }
        }

        if (!errors.isEmpty()) {
            throw new ImportRejectedException(errors);
        }
        return new ProjectGraph(categories, functors, naturalTransformations);
    }

    private static List<CategoryObject> objects(CategoryDefinition category, String categoryAt,
            Set<String> objectIds, Map<String, DocumentDefinition> contents, List<String> errors) {
        var objects = new ArrayList<CategoryObject>();
        for (int j = 0; j < category.objects().size(); j++) {
            var object = category.objects().get(j);
            var at = "%s.objects[%d]".formatted(categoryAt, j);
            if (object == null) {
                errors.add(at + ": object is empty");
                continue;
            }
            boolean valid = require(errors, at, "id", object.id())
                    & require(errors, at, "title", object.title())
                    & require(errors, at, "domain", object.domain());
            if (object.id() != null && !objectIds.add(object.id())) {
                errors.add("Duplicate object id '%s'".formatted(object.id()));
            }
            if (valid) {
                var document = contents.get(object.id());
                objects.add(new CategoryObject(object.id(), object.title(), object.domain(), object.metadata(),
                        document == null ? "" : document.body()));
            }
        }
        return objects;
    }

    private static List<Morphism> morphisms(CategoryDefinition category, String categoryAt, List<String> errors) {
        var morphisms = new ArrayList<Morphism>();
        var morphismIds = new HashSet<String>();
        for (int k = 0; k < category.morphisms().size(); k++) {
            var morphism = category.morphisms().get(k);
            var at = "%s.morphisms[%d]".formatted(categoryAt, k);
            if (morphism == null) {
                errors.add(at + ": morphism is empty");
                continue;
            }
            boolean valid = require(errors, at, "id", morphism.id())
                    & require(errors, at, "name", morphism.name())
                    & require(errors, at, "source", morphism.source())
                    & require(errors, at, "target", morphism.target());
            if (morphism.id() != null && !morphismIds.add(morphism.id())) {
                errors.add("Duplicate morphism id '%s' in category '%s'".formatted(morphism.id(), category.id()));
            }
            if (valid) {
                morphisms.add(new Morphism(morphism.id(), morphism.name(), morphism.source(), morphism.target(),
                        morphism.metadata()));
            }
        }
        return morphisms;
    }

    private record Documents(Map<String, DocumentDefinition> byObject, List<String> duplicates) {
    }

    /** パスの順に並べ、同じ文書を指す Markdown が2つあれば誤りにする。 */
    private static Documents attachDocuments(ProjectDefinition definition) {
        var byObject = new LinkedHashMap<String, DocumentDefinition>();
        var duplicates = new ArrayList<String>();
        definition.documents().stream()
                .sorted(Comparator.comparing(DocumentDefinition::path))
                .forEach(document -> {
                    var previous = byObject.putIfAbsent(document.objectId(), document);
                    if (previous != null) {
                        duplicates.add("'%s' and '%s' both refer to object '%s'"
                                .formatted(previous.path(), document.path(), document.objectId()));
                    }
                });
        return new Documents(byObject, duplicates);
    }

    private static boolean require(List<String> errors, String at, String field, String value) {
        if (value == null || value.isBlank()) {
            errors.add("%s: '%s' is required".formatted(at, field));
            return false;
        }
        return true;
    }

    private static void refer(List<String> errors, Set<String> known, String id, String message) {
        if (!known.contains(id)) {
            errors.add(message);
        }
    }
}
