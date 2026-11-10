import type { FlatResponseData } from '@sa/axios';
import type { PaginationData } from '@sa/hooks';

/** The page envelope every list endpoint answers with. */
export interface PageEnvelope<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

/**
 * Translate the backend page into the table hook's shape.
 *
 * The two differ in field names only: biw answers `records / total / page / size / pages`,
 * while the hook expects `data / total / pageNum / pageSize`.
 */
export function transformPage<T>(response: FlatResponseData<any, PageEnvelope<T>>): PaginationData<T> {
  const { data, error } = response;

  if (!error && data) {
    return {
      data: data.records,
      pageNum: data.page,
      pageSize: data.size,
      total: data.total
    };
  }

  return {
    data: [],
    pageNum: 1,
    pageSize: 10,
    total: 0
  };
}
