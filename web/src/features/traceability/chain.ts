import type { FunctorSummary } from '../../api/queries'

/**
 * 関手の列の次に置ける関手。列が空ならすべて、そうでなければ前の関手の先から始まるもの。
 * 一度使った関手は除く。自分のカテゴリに戻る関手を何度も選べると、列がいくらでも伸びるため。
 */
export function nextCandidates(functors: FunctorSummary[], chain: string[]): FunctorSummary[] {
  if (chain.length === 0) return functors
  const last = functors.find((f) => f.id === chain[chain.length - 1])
  if (last === undefined) return []
  return functors.filter((f) => f.sourceCategory === last.targetCategory && !chain.includes(f.id))
}
