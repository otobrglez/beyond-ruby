#!/usr/bin/env ruby
# frozen_string_literal: true
$LOAD_PATH << './stations-rb/'
%w[bundler/setup json ostruct http pry distance.rb env.rb].each(&method(:require))

class StationsException < StandardError; end
class ProminfoException < StationsException; end
class PositionstackException < StationsException; end
class OpenrouteException < StationsException; end

class Stations
  class << self
    def bike_stations
      Env.prominfo_query_endpoint
         .then { HTTP.get(_1, params: Env.prominfo_params) }
         .then { JSON.parse(_1, symbolize_names: true) }
         .then { _1.fetch(:features, []) }
         .flat_map { |station| station.slice(:attributes, :geometry).values.inject(&:merge) }
         .filter_map do |station|
        next unless station[:bike_stand_free] > 0

        station.slice(:name, :bike_stand_free, :x, :y)
               .transform_keys(x: :longitude, y: :latitude)
      end or raise ProminfoException.new("Failed fetching stations from prominfo service.")
    end

    def location_of(query)
      Env.positionstack_forward_endpoint
         .then { [_1, { access_key: Env.positionstack_api_key, query: query }] }
         .then { HTTP.get(_1, params: _2) }
         .then { JSON.parse(_1, symbolize_names: true) }
         .then { [_1.fetch(:data, []).first] }
         .filter_map do |location|
        next if location.nil?

        location.slice(:name, :longitude, :latitude)
      end
         .first or raise PositionstackException.new("Could not resolve \"#{query}\" to location.")
    end

    def near(query, size = 3)
      location = Stations.location_of(query)
      stations = Stations.bike_stations
      stations
        .map { |s| s.merge(distance: Distance.geo(s, location)) }
        .sort_by { _1.fetch(:distance) }
        .take(size)
    end

    def near_par(query, size = 3)
      (stations, location) = [
        Thread.new { Stations.bike_stations },
        Thread.new { Stations.location_of(query) }
      ].map(&:value)

      stations.map { |s| s.merge(distance: Distance.geo(s, location)) }
              .sort_by { _1[:distance] }
              .take(size)
    end

    def directions(from, to, profile: 'driving-car')
      Env.openroute_directions_endpoint.then do |endpoint|
        HTTP.get(endpoint + "/#{profile}", params: {
          api_key: Env.openroute_api_key,
          start: [from.fetch(:longitude), from.fetch(:latitude)].join(','),
          end: [to.fetch(:longitude), to.fetch(:latitude)].join(',')
        })
            .then { JSON.parse(_1, symbolize_names: true) }
            .then { _1.fetch(:features, []) }
            .flat_map { _1.fetch(:properties, []) }
            .flat_map { _1.fetch(:summary, {}) }
            .first
      end or raise OpenrouteException.new("Failed getting directions from openroute service.")
    end

    def near_duration(query, size = 3)
      location = Stations.location_of(query)
      stations = Stations.bike_stations
      close_stations = stations
                         .map { |s| s.merge(distance: Distance.geo(s, location)) }
                         .sort_by { _1[:distance] }
                         .take((size * 1.6).ceil)

      close_stations.map do |station|
        station.merge(Stations.directions(location, station))
      end
                    .sort_by { _1[:duration] }.take(size)
    end

    def near_duration_par(query, size = 3)
      Thread.abort_on_exception = true

      (stations, location) = [
        Thread.new { Stations.bike_stations },
        Thread.new { Stations.location_of(query) }
      ].map(&:value)

      close_stations = stations
                         .map { |s| s.merge(distance: Distance.geo(s, location)) }
                         .sort_by { _1[:distance] }
                         .take((size * 1.6).ceil)

      close_stations.map do |station|
        Thread.new do
          station.merge(Stations.directions(location, station))
        end
      end.map(&:value)
                    .sort_by { _1[:duration] }
                    .take(size)
    end
  end
end