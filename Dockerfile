FROM rappdw/docker-java-python

LABEL maintainer <maxat.kulmanov@kaust.edu.sa>

USER root

RUN mkdir /tmp/phenomenet-vp
WORKDIR /tmp/phenomenet-vp

# Install gradle
RUN curl -L https://downloads.gradle.org/distributions/gradle-4.10.2-bin.zip -o gradle.zip && \
  mkdir /opt/gradle && \
  unzip -d /opt/gradle gradle.zip && \
  rm -rf *

ENV PATH="/opt/gradle/gradle-4.10.2/bin:${PATH}"

COPY . .

RUN pip install -r requirements.txt

# Should be the same as in build.gradle
ENV pvp_version='2.1'

RUN gradle assembleDist && ls build/distributions/ && \
  unzip build/distributions/phenomenet-vp-${pvp_version}.zip -d /app/ && \
  rm -rf *

ENV PATH="/app/phenomenet-vp-${pvp_version}/bin:${PATH}"

WORKDIR /

ENTRYPOINT ["phenomenet-vp"]