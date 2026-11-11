export type Template = {
  id: string;
  title: string;
  category: string;
  description: string;
  coverUrl: string;
  requiredImageCount: number;
  maxFileSizeBytes: number;
  acceptedMimeTypes: string[];
};
