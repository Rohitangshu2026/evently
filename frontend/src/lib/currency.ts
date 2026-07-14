/**
 * Formats a numeric amount as Indian Rupees, e.g. 1500 → "₹1,500".
 * Prices are stored currency-agnostically on the backend (plain decimals);
 * this is the single place the app decides how money is displayed.
 */
export const formatInr = (amount?: number | null): string => {
  if (amount === undefined || amount === null) return "—";
  return `₹${amount.toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
};
