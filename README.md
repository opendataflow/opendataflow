# opendataflow
An open, extendable framework to build data pipelines

## Concepts

When working on data projects there is a big divide between
big companies and smaller ones. Big companies have usually
multiple teams to work in operations, development, testing.
In smaller companies, it's often one person who does pretty
much all the above.

The goal of this framework is to use abstraction to clearly
separate concerns - dev, test, operations - while being
fairly lightweight, that is, easy to learn and use.

We'd also like the data flows being self-documenting, 'forcing'
the developer to include a minimal amount of documentation.
We all have worked on a project written by somebody who didn't
comment his code and did not write documentation, don't be that
guy.

Also, we'd like to build reusable data components, an equivalent
of the 'library' in the data world. Once you have a number of
components, we'd like to be just able to compose them using
a configuration file, hence write new pipelines without
writing a line of programming language, but just a config
file, maybe even using a GUI, so non-programmers can
build, test and deploy pipelines, needing a developer only to
write new components.

So, the main concepts are:

- Pipeline Component (PComponent class): a generic data component,
  that has an implementation and Connectors (both input and output).
  in our library analogy, it's like a function with input and outputs
- AbstractData: a generic descripion of the data that a component
  accepts and produces. In our analogy, this is the equivalent to a
  type system. In the data world, we have a few things we want to cover.
  First is bound/unbound data, that is, table or a stream. A stream is
  unbound because we don't know its bounds.
- Composition: components can be assembled together in larger components.
  a Pipeline is itself a component.
- ExecutionEngine: once we have the high level implementation of the
  pipeline, it needs to be executed on an engine. The first implementation
  will be with spark, but the idea is to be able to implement different
  runners as needed.

