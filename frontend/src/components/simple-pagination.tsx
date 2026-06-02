import { SpringBootPagination } from "@/domain/domain";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface SimplePaginationProps<T> {
  pagination: SpringBootPagination<T>;
  onPageChange: (page: number) => void;
}

export function SimplePagination<T>({
  pagination,
  onPageChange,
}: SimplePaginationProps<T>) {
  const currentPage = pagination.number;
  const totalPages = pagination.totalPages;

  const btn =
    "group inline-flex h-9 w-9 items-center justify-center rounded-full border border-border bg-card text-foreground transition-all hover:border-gold hover:text-gold disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-border disabled:hover:text-foreground";

  return (
    <div className="flex items-center gap-5">
      <button
        className={cn(btn)}
        onClick={() => onPageChange(currentPage - 1)}
        disabled={pagination.first}
        aria-label="Previous page"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <div className="font-display text-sm tracking-[0.12em] text-muted-foreground">
        <span className="text-foreground">{String(currentPage + 1).padStart(2, "0")}</span>
        <span className="mx-3 text-border">/</span>
        <span>{String(totalPages).padStart(2, "0")}</span>
      </div>
      <button
        className={cn(btn)}
        onClick={() => onPageChange(currentPage + 1)}
        disabled={pagination.last}
        aria-label="Next page"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );
}
