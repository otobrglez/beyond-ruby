#!/usr/bin/env bash
set -ex

get_stations() {
  geometry='{"xmin":14.0321,"ymin":45.7881,"xmax":14.8499,"ymax":46.218,"spatialReference":{"wkid":4326}}'
  curl -G \
    --data-urlencode "f=json" \
    --data-urlencode "returnGeometry=true" \
    --data-urlencode "outSr=4326" \
    --data-urlencode "geometry=$geometry}" \
    --data-urlencode "outFields=*" \
    -s --insecure 'https://prominfo.projekti.si/web/api/MapService/Query/lay_bicikelj/query'
}

stations_list() {
  get_stations |
    jq -c ".features[]|[.geometry.y,.geometry.x,.attributes.bike_stand_free,.attributes.name]| select(.[2] != 0)" |
    cat | sed 's/ *$//g' | tr '[] ' " " | sed "s/ //1"
}

stations_list
