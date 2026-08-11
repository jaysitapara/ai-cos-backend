package com.app.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TechnologyCatalogService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechOption {
        private String id;
        private String name;
        private String category;
        private String description;
        private boolean isDefault;
        private List<String> compatibility;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechQuestionDefinition {
        private String key;
        private String title;
        private String description;
        private String category;
        private List<TechOption> options;
        private String defaultOptionId;
    }

    public List<TechQuestionDefinition> getStandardCatalogQuestions() {
        List<TechQuestionDefinition> questions = new ArrayList<>();

        // 1. Frontend
        questions.add(TechQuestionDefinition.builder()
                .key("frontend")
                .title("Frontend Framework & Tech")
                .description("Select the user interface framework for the application.")
                .category("FRONTEND")
                .defaultOptionId("nextjs")
                .options(List.of(
                        new TechOption("nextjs", "Next.js (React 18 / App Router)", "FRONTEND", "Server-rendered React framework with SSR and API routes", true, List.of("nestjs", "express", "fastapi", "django", "springboot", "go")),
                        new TechOption("react", "React (Vite SPA)", "FRONTEND", "Client-side single page app built with React & Vite", false, List.of("nestjs", "express", "fastapi", "django", "springboot", "go")),
                        new TechOption("vue", "Vue 3 (Vite SPA)", "FRONTEND", "Progressive JavaScript framework", false, List.of("nestjs", "express", "fastapi", "springboot")),
                        new TechOption("nuxt", "Nuxt.js (Vue 3 SSR)", "FRONTEND", "Intuitive Vue framework for SSR applications", false, List.of("nestjs", "express", "fastapi")),
                        new TechOption("angular", "Angular 17", "FRONTEND", "Enterprise TypeScript web app platform", false, List.of("springboot", "nestjs", "express")),
                        new TechOption("other", "Other / Custom", "FRONTEND", "Specify your custom frontend technology", false, List.of())
                ))
                .build());

        // 2. Backend
        questions.add(TechQuestionDefinition.builder()
                .key("backend")
                .title("Backend Architecture & Runtime")
                .description("Select the server application framework and API engine.")
                .category("BACKEND")
                .defaultOptionId("nestjs")
                .options(List.of(
                        new TechOption("nestjs", "Node.js / NestJS (TypeScript)", "BACKEND", "Modular enterprise Node.js framework", true, List.of("postgres", "mysql", "mongodb")),
                        new TechOption("express", "Node.js / Express", "BACKEND", "Lightweight web framework for Node.js", false, List.of("postgres", "mysql", "mongodb", "sqlite")),
                        new TechOption("fastapi", "Python / FastAPI", "BACKEND", "Modern fast Python 3.10+ web framework with OpenAPI", false, List.of("postgres", "mysql", "sqlite")),
                        new TechOption("django", "Python / Django", "BACKEND", "High-level Python web framework with ORM", false, List.of("postgres", "mysql")),
                        new TechOption("springboot", "Java / Spring Boot 3", "BACKEND", "Enterprise Java framework with Spring Data JPA", false, List.of("postgres", "mysql")),
                        new TechOption("go", "Go (Gin / Fiber)", "BACKEND", "High-performance microservices backend", false, List.of("postgres", "mysql", "sqlite")),
                        new TechOption("other", "Other / Custom", "BACKEND", "Specify your custom backend engine", false, List.of())
                ))
                .build());

        // 3. Database
        questions.add(TechQuestionDefinition.builder()
                .key("database")
                .title("Primary Database Engine")
                .description("Select the persistent data storage system.")
                .category("DATABASE")
                .defaultOptionId("postgres")
                .options(List.of(
                        new TechOption("postgres", "PostgreSQL", "DATABASE", "Advanced open-source relational SQL database", true, List.of("jwt", "session", "oauth", "authjs")),
                        new TechOption("mysql", "MySQL", "DATABASE", "Popular relational relational database system", false, List.of("jwt", "session", "oauth")),
                        new TechOption("mongodb", "MongoDB", "DATABASE", "NoSQL document store for flexible JSON schemas", false, List.of("jwt", "oauth")),
                        new TechOption("sqlite", "SQLite", "DATABASE", "Embedded file-based SQL database for rapid prototyping", false, List.of("jwt", "session")),
                        new TechOption("other", "Other / Custom", "DATABASE", "Specify custom database engine", false, List.of())
                ))
                .build());

        // 4. Authentication
        questions.add(TechQuestionDefinition.builder()
                .key("authentication")
                .title("Authentication & Authorization")
                .description("Select the security and identity authentication model.")
                .category("AUTHENTICATION")
                .defaultOptionId("jwt")
                .options(List.of(
                        new TechOption("jwt", "JWT Token Auth", "AUTHENTICATION", "Stateless JSON Web Tokens with Refresh Token Rotation", true, List.of("tailwind", "css_modules")),
                        new TechOption("authjs", "Auth.js / NextAuth", "AUTHENTICATION", "Complete open source authentication for Web", false, List.of("tailwind")),
                        new TechOption("oauth", "OAuth 2.0 / Google SSO", "AUTHENTICATION", "Social login integration with OAuth2 providers", false, List.of("tailwind")),
                        new TechOption("session", "Session-based Cookies", "AUTHENTICATION", "Stateful server session cookies with Redis", false, List.of("tailwind")),
                        new TechOption("other", "Other / Custom", "AUTHENTICATION", "Specify custom auth mechanism", false, List.of())
                ))
                .build());

        // 5. Styling
        questions.add(TechQuestionDefinition.builder()
                .key("styling")
                .title("UI & Styling Framework")
                .description("Select CSS styling framework and component styling.")
                .category("STYLING")
                .defaultOptionId("tailwind")
                .options(List.of(
                        new TechOption("tailwind", "Tailwind CSS + shadcn/ui", "STYLING", "Utility-first CSS framework with accessible components", true, List.of("zustand", "redux", "react_query")),
                        new TechOption("css_modules", "CSS Modules", "STYLING", "Scoped CSS styling per component", false, List.of("redux")),
                        new TechOption("mui", "Material UI (MUI v5)", "STYLING", "Google Material Design React component library", false, List.of("redux")),
                        new TechOption("other", "Other / Custom", "STYLING", "Specify custom UI/CSS framework", false, List.of())
                ))
                .build());

        return questions;
    }
}
