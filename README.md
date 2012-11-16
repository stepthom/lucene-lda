lucene-lda
==========

Using latent Dirichlet allocation (LDA) in Apache Lucene

AUTHOR
------

[Stephen W. Thomas](http://research.cs.queensu.ca/~sthomas/) <<sthomas@cs.queensu.ca>>


DESCRIPTION
----------- 
lucene-lda allows users to build indexes and perform queries using latent
Dirichlet allocation (LDA), an advanced topic model, within the Lucene
framework.

lucene-lda was originally developed as part of a research project that compared the
performance of the Vector Space Model (VSM), which is Lucene's default IR model,
with the performace of LDA. The context was bug localization, where the goal is
to determine the similarity between bug reports and source code files. However,
lucene-lda is general enough that other contexts can be considered: as long as
there are (a) input documents to be searched and (b) queries to be executed. 

lucene-lda can work in two different ways:

* You have already executed LDA on the input corpus, and you feed to the resultant
topics and topic memberships to lucene-lda. In this case, lucene-lda will
internalize the topics and topic memberships while building the index and
executing the queries. (You can even input multiple LDA executions, for example
if you have run LDA with different parameters. Here, you specify and query time
which set of parameters you would like to use.) Specifically, you need to
specify four files, for each parameter LDA parameter combination:
  * `vocab.dat`: a Vx1 list of terms in the corpus
  * `words.dat`: a KxV matrix (white-space delimited) that specifies the
    membership of each word in each topic.
  * `files.dat:` A Dx3 matrix (white-space) that lists the original file
    names that LDA was executed on. The first and third columns are ignored; the
    second column should contain the file name.
  * `theta.dat`: A DxK matrix (white-space) tat specifies the topic membership of
    each file in each topic.

In the above, V is the number of terms; K is the number of topics; and D is the
number of documents. The order of the terms in `vocab.dat` should match the order
in `words.dat`; the same is true for the filenames in `files.dat` and `theta.dat`.

* You have not yet run LDA on the input corpus, and you feed only the raw documents
to lucene-lda. In this case, lucene-lda will first execute LDA on the documents
(using MALLET), and then build the index using the resultant topics and topic memberships.

In either case, you can specify at query time if you want to use the VSM model
or LDA model for executing a particular query. lucene-lda will then return a
ranked list of documents that best match the given query.


USAGE
-----

Use on the command line:

    bin/indexDirectory TODO

    bin/queryAllInDirectory TODO




INSTALLATION
------------

    ant jar
    ant test


DEPENDENCIES
------------

lucene-lda depends on Apache Lucene, MALLET, Apache Commons, and JUnit. All are
included in the lib/ directory.


COPYRIGHT AND LICENCE
---------------------

Copyright (C) 2012 by Stephen W. Thomas <sthomas@cs.queensu.ca>

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.10.1 or,
at your option, any later version of Perl 5 you may have available.


