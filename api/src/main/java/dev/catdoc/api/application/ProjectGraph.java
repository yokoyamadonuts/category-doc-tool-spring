package dev.catdoc.api.application;

import dev.catdoc.domain.Category;
import dev.catdoc.domain.CategoryObject;
import dev.catdoc.domain.Functor;
import dev.catdoc.domain.NaturalTransformation;
import java.util.List;
import java.util.Optional;

/** 1つのプロジェクトのカテゴリ、関手、自然変換。 */
public record ProjectGraph(List<Category> categories, List<Functor> functors,
        List<NaturalTransformation> naturalTransformations) {

    public ProjectGraph {
        categories = List.copyOf(categories);
        functors = List.copyOf(functors);
        naturalTransformations = List.copyOf(naturalTransformations);
    }

    public Optional<Category> category(String categoryId) {
        return categories.stream().filter(c -> c.id().equals(categoryId)).findFirst();
    }

    public Optional<Functor> functor(String functorId) {
        return functors.stream().filter(f -> f.id().equals(functorId)).findFirst();
    }

    /** 文書の ID はプロジェクトの中で一意なので、文書を含むカテゴリは1つに決まる。 */
    public Optional<Category> categoryOf(String objectId) {
        return categories.stream().filter(c -> c.object(objectId).isPresent()).findFirst();
    }

    public Optional<CategoryObject> object(String objectId) {
        return categoryOf(objectId).flatMap(c -> c.object(objectId));
    }
}
