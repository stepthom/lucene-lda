lucene-lda
==========

Use latent Dirichlet allocation (LDA) in Apache Lucene

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
with the performance of LDA. The context was bug localization, where the goal is
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
(NOTE: this scenario is not yet implemented.)

In either case, you can specify at query time if you want to use the VSM model
or LDA model for executing a particular query. lucene-lda will then return a
ranked list of documents that best match the given query.

lucene-lda assumes that any complicated preprocessing of the documents or
queries has already been performed. See
[lscp][https://github.com/doofuslarge/lscp] for a nice preprocessor.


DESIGN NOTES
------------

A bit of background.

By default, Lucene uses a slight variant of the vector space model (VSM) to
compute the similarity between a query and each document in the index. (There
are some bells and whistles that are available, but this is the general idea.)
The basic formulation of the similarity comes from the cosine distance between
two vectors: one for the document, and one for the query. The numbers in the
vector are the _term weights_ of each term in the document and query.

LDA works very differently. In the LDA model, similarity is computed using
_conditional probability_, which not only involves the terms of the query and
document, but also the _topics_ in the query and documents. Basically, we needed
a way to store which topics are in each document in Lucene. To do so, we use
Payloads to cleverly encode the topics in each document at index time. Then, at
query time, we do the following.
* Determine which topics are in the query, based on the terms in the query
* Create a Payload query based on these topics
* Lucene will then find all documents that contain these topics.
* We ignore the actual relevancy returned by Lucene, and instead use the
 contents of the Payload to compute the relevancy ourselves, and re-rank the
 results.

Two notes about similarity:

* In the above process, performance is actually fast for computing conditional
probability, since we are only computing it for those documents that have some
of the topics in the query, as opposed to every document in the index.

* We have created an LDAHelper() class that holds necessary values related to
  LDA, such as the theta and phi matrices returned by LDA. These values are
  necessary to compute conditional probability, but are impractical to store
  along with every document in the index. Currently, these values are written to
  disk during indexing as a separate "LDA index", and then read into memory
  again at query time. A potential improvement is to add these matrices to the
  Lucene index somehow, in a space and time efficient manner.



USAGE
-----

Use on the command line:

    bin/indexDirectory [--help] <inDir> <outIndexDir> <outLDAIndex> [--fileCodes
    <fileCodes>] [--ldaConfig ldaConfig1,ldaConfig2,...,ldaConfigN ]

    bin/queryWithVSM [--help] <indexDir> <queryDir> <resultsDir>
    [--weightingCode <weightingCode>] [--scoringCode <scoringCode>] 

    bin/queryWithLDA [--help] <indexDir> <LDAIndexDir> <queryDir> <resultsDir>
    [--K <K>] [--scoringCode <scoringCode>]

The above scripts simply call the corresponding Java classes, after setting the
classpath as needed.





BUILD AND INSTALLATION
----------------------

Simply type:

    ant jar
    ant test


DEPENDENCIES
------------

lucene-lda depends on Apache Lucene, MALLET, Apache Commons, Apache log4j, JSAP, and JUnit. All are
included in the lib/ directory.


COPYRIGHT AND LICENCE
---------------------

Copyright (C) 2012 by Stephen W. Thomas <sthomas@cs.queensu.ca>



