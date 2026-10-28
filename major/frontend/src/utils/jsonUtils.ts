/**
 * JSON parsing utilities — defined in a .ts file to avoid JSX generic conflicts.
 */
import type { SuspiciousRegion } from '../types';

export function safeParseJson<T>(json: string | null | undefined): T | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}

export function parseRegions(json: string | null | undefined): SuspiciousRegion[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json);
    return Array.isArray(arr) ? (arr as SuspiciousRegion[]) : [];
  } catch {
    return [];
  }
}

export function parseStringArray(json: string | null | undefined): string[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json);
    return Array.isArray(arr) ? (arr as string[]) : [];
  } catch {
    return [];
  }
}

export function parseObject(json: string | null | undefined): Record<string, unknown> {
  if (!json) return {};
  try {
    const obj = JSON.parse(json);
    return (obj && typeof obj === 'object' && !Array.isArray(obj))
      ? (obj as Record<string, unknown>)
      : {};
  } catch {
    return {};
  }
}

export function parseArray(json: string | null | undefined): unknown[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}
