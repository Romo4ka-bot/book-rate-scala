package infrastructure.endpoint

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object Pagination {
  import QueryParamDecoder._

  object OptionalPageSizeMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")
  object OptionalOffsetMatcher extends OptionalQueryParamDecoderMatcher[Int]("size")
}
