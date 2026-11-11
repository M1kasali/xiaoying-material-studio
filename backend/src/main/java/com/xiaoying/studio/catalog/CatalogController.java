package com.xiaoying.studio.catalog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final TemplateMapper templates;

    public CatalogController(TemplateMapper templates) { this.templates = templates; }

    @GetMapping("/categories")
    public List<String> categories() { return templates.findCategories(); }

    @GetMapping("/templates")
    public List<TemplateResponse> list(@RequestParam(required = false) String category) {
        return templates.findAll(category).stream().map(TemplateResponse::from).toList();
    }

    @GetMapping("/templates/{id}")
    public TemplateResponse detail(@PathVariable String id) {
        var template = templates.findById(id);
        if (template == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "模板不存在");
        return TemplateResponse.from(template);
    }

    public record TemplateResponse(String id, String title, String category, String description,
            String coverUrl, int requiredImageCount, long maxFileSizeBytes, List<String> acceptedMimeTypes) {
        static TemplateResponse from(Template t) {
            return new TemplateResponse(t.id(), t.title(), t.category(), t.description(), t.coverUrl(),
                    t.requiredImageCount(), t.maxFileSizeBytes(), List.of("image/jpeg", "image/png"));
        }
    }
}
