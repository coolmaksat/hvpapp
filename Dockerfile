FROM rappdw/docker-java-python

LABEL maintainer <maxat.kulmanov@kaust.edu.sa>

USER root

WORKDIR /tmp

COPY . .

RUN curl https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v2.0/phenomenet-vp-2.0.zip -o phenoment.zip && \
  unzip phenomenet.zip -d /app/ && \
  curl http://bio2vec.net/pvp/data-v2.0.tar.gz -o data.tar.gz && \
  tar xvzf data.tar.gz -C /app/phenoment-vp-2.0/
  rm -rf *

ENV PATH="/app/phenomenet-vp-2.0/bin:${PATH}"

WORKDIR /

ENTRYPOINT ["phenomenet-vp"]