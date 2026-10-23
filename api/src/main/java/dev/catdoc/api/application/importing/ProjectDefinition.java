package dev.catdoc.api.application.importing;

import java.util.List;

/**
 * category.yaml と Markdown から読んだ、確かめる前の定義。
 * 項目の欠けを取り込みの誤りとして並べるため、どの項目も null を許す。
 */
public record ProjectDefinition(List<CategoryDefinition> categories, List<FunctorDefinition> functors,
        List<NaturalTransformationDefinition> naturalTransformations, List<DocumentDefinition> documents) {

    public ProjectDefinition {
        categories = Lists.orEmpty(categories);
        functors = Lists.orEmpty(functors);
        naturalTransformations = Lists.orEmpty(naturalTransformations);
        documents = Lists.orEmpty(documents);
    }
}
