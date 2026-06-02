import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/utils";

interface RandomEventImageProps {
  seed?: string;
  className?: string;
}

const RandomEventImage: React.FC<RandomEventImageProps> = ({
  seed,
  className,
}) => {
  const [imageSrc, setImageSrc] = useState("");

  const index = useMemo(() => {
    if (!seed) return null;
    let hash = 0;
    for (let i = 0; i < seed.length; i++) {
      hash = (hash * 31 + seed.charCodeAt(i)) | 0;
    }
    return (Math.abs(hash) % 4) + 1;
  }, [seed]);

  useEffect(() => {
    const i = index ?? Math.floor(Math.random() * 4) + 1;
    setImageSrc(`/event-image-${i}.webp`);
  }, [index]);

  return (
    <div className={cn("relative h-full w-full overflow-hidden", className)}>
      <img
        src={imageSrc}
        alt=""
        className="h-full w-full object-cover transition-transform duration-[1200ms] ease-out group-hover:scale-[1.04]"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-gradient-to-t from-ink/35 via-ink/0 to-transparent"
      />
    </div>
  );
};

export default RandomEventImage;
