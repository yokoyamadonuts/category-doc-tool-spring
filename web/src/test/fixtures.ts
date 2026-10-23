import type { components } from '../api/schema'

type Schemas = components['schemas']

export const PROJECT_ID = '0f8c3b5e-1d2a-4c7b-9e6f-123456789abc'
export const BASE = `/api/projects/${PROJECT_ID}`

/** dev-project を縮めたもの。要件、仕様、インフラの3層。 */
export const project: Schemas['ProjectDetail'] = {
  id: PROJECT_ID,
  name: 'dev-project',
  importedAt: '2026-10-23T01:02:03Z',
  categories: [
    { id: 'requirements', name: 'Requirements', objectCount: 2, morphismCount: 3 },
    { id: 'specifications', name: 'Specifications', objectCount: 2, morphismCount: 3 },
    { id: 'infrastructure', name: 'Infrastructure', objectCount: 3, morphismCount: 6 },
  ],
  functors: [
    { id: 'specify', name: 'Specify', sourceCategory: 'requirements', targetCategory: 'specifications' },
    { id: 'deploy', name: 'Deploy', sourceCategory: 'specifications', targetCategory: 'infrastructure' },
    { id: 'self', name: 'Self', sourceCategory: 'requirements', targetCategory: 'requirements' },
  ],
  naturalTransformations: [],
}

const morphism = (id: string, name: string, source: string, target: string): Schemas['MorphismView'] => ({
  id, name, source, target, identity: source === target,
})

export const infrastructureGraph: Schemas['CategoryGraph'] = {
  id: 'infrastructure',
  name: 'Infrastructure',
  objects: [
    { id: 'infra-user', title: 'ユーザーサービス', domain: 'infrastructure' },
    { id: 'infra-auth', title: '認証サービス', domain: 'infrastructure' },
    { id: 'infra-db', title: 'データベース', domain: 'infrastructure' },
  ],
  morphisms: [
    morphism('user-db', 'connects', 'infra-user', 'infra-db'),
    morphism('auth-db', 'connects', 'infra-auth', 'infra-db'),
    morphism('auth-user', 'depends', 'infra-auth', 'infra-user'),
    morphism('id-user', 'id_user', 'infra-user', 'infra-user'),
    morphism('id-auth', 'id_auth', 'infra-auth', 'infra-auth'),
    morphism('id-db', 'id_db', 'infra-db', 'infra-db'),
  ],
}

export const requirementsGraph: Schemas['CategoryGraph'] = {
  id: 'requirements',
  name: 'Requirements',
  objects: [
    { id: 'req-user', title: 'ユーザー管理要件', domain: 'requirements' },
    { id: 'req-auth', title: '認証システム要件', domain: 'requirements' },
  ],
  morphisms: [morphism('auth-user', 'depends-on', 'req-auth', 'req-user')],
}

export const authObject: Schemas['ObjectDetail'] = {
  id: 'req-auth',
  title: '認証システム要件',
  domain: 'requirements',
  categoryId: 'requirements',
  metadata: { priority: 'high', stakeholder: 'セキュリティチーム', tags: ['auth', 'security'] },
  content: '# 認証システム要件\n\n## 概要\nログインの要件。\n',
}

const entry = (id: string, name: string, errors: string[] = [], warnings: string[] = []): Schemas['VerificationEntry'] => ({
  id, name, valid: errors.length === 0, errors, warnings,
})

export const validReport: Schemas['VerificationReport'] = {
  valid: true,
  categories: [entry('requirements', 'Requirements'), entry('specifications', 'Specifications')],
  functors: [entry('specify', 'Specify')],
  naturalTransformations: [],
}

export const invalidReport: Schemas['VerificationReport'] = {
  valid: false,
  categories: [
    entry('requirements', 'Requirements', ["Object 'req-auth' lacks an identity morphism (id: req-auth → req-auth)"]),
    entry('specifications', 'Specifications', [], ['Missing composition: g∘f (a → c)']),
  ],
  functors: [entry('specify', 'Specify', ["Object 'req-user' is not mapped by functor 'specify'", 'second error'])],
  naturalTransformations: [],
}

export const authToDbPaths: Schemas['PathResult'] = {
  from: 'infra-auth',
  to: 'infra-db',
  categoryId: 'infrastructure',
  shortest: [morphism('auth-db', 'connects', 'infra-auth', 'infra-db')],
  all: [
    [morphism('auth-db', 'connects', 'infra-auth', 'infra-db')],
    [morphism('auth-user', 'depends', 'infra-auth', 'infra-user'), morphism('user-db', 'connects', 'infra-user', 'infra-db')],
  ],
}

export const traceTable: Schemas['TraceabilityTable'] = {
  functors: [project.functors[0], project.functors[1]],
  rows: [
    { objectId: 'req-user', title: 'ユーザー管理要件', mappedObjectIds: ['spec-user', 'infra-user'], complete: true },
    { objectId: 'req-auth', title: '認証システム要件', mappedObjectIds: ['spec-auth'], complete: false },
  ],
}
