# KnotResearch Backend

The backend for KnotResearch, an interactive research tool for exploring knots and studying the behavior of knot invariants.

## Overview

The KnotResearch backend provides the REST API and data infrastructure for the KnotResearch application.

It is responsible for retrieving and storing knot data, generating structured knot diagrams, providing diagram information to the frontend, and coordinating mathematical computations.

A central goal of the project is to represent knot diagrams as structured mathematical objects rather than as static images. The backend therefore stores information about crossings, strands, orientations, connectivity, and other properties needed to reconstruct and manipulate diagrams computationally.

The backend is also being developed as the foundation for future research experiments involving sequences of diagram transformations and comparisons of knot invariants.

## Current Features

### Knot Data

The backend provides access to knots from the Rolfsen table up to 13 crossings, along with associated diagram and invariant data.

### Structured Diagram Representation

Knot diagrams are represented using structured data describing their underlying combinatorial information.

This includes:

* Crossings
* Strands
* Over/under placement
* Strand connectivity
* Orientation
* Crossing signs
* Diagram coordinates

This representation allows the frontend to reconstruct and visualize diagrams while preserving the mathematical information needed for future diagram manipulation.

### Diagram Generation

The backend generates the structured information required to construct oriented knot diagrams.

Rather than storing a diagram only as an image, the system stores the relationships necessary to reconstruct the diagram and operate on it computationally.

### Invariant Data

The backend stores and serves computed knot invariants associated with the knots in the database.

The invariant data is intended to provide the basis for comparing knots and studying how invariants behave as diagrams are transformed.

The backend also interfaces with mathematical computing tools used for knot data and invariant calculations, including Python, SageMath, and SnapPy.

## Technology

* Java 17
* Spring Boot
* Maven
* PostgreSQL
* Supabase
* Python
* SageMath
* SnapPy

## API

The backend exposes REST endpoints used by the frontend to retrieve knot and diagram information and perform backend operations.

API documentation is available through Swagger/OpenAPI when running the backend locally.

## Data Model

The database separates different aspects of knot and diagram information into structured tables.

Among other data, the system stores information relating to:

* Knots
* Knot diagrams
* Crossings
* Vertices and arrows
* Strand connectivity
* Full diagram notation
* Knot invariants

This separation allows the application to reconstruct diagrams and work with their underlying combinatorial structure rather than relying on rendered images.

## Research Direction

The backend is being developed to support computational experiments in knot theory.

The intended workflow is to allow sequences of transformations to be applied to individual knots or collections of knots while preserving the intermediate states:

Future work includes implementing diagram transformations such as smoothing, saddle moves, and Seifert-ification, as well as building infrastructure for applying sequences of transformations to collections of knots and exporting the resulting data for analysis.

The backend is currently deployed on Render.

The free hosting tier may cause the service to take a moment to start after a period of inactivity.

## Related Repository

The frontend for this project is available in the https://github.com/DylanZimmer/KnotResearchFrontend repository.
