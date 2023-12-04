#!/usr/bin/env ruby
# frozen_string_literal: true

%w[bundler/setup json ostruct http sinatra sinatra/json ./distance.rb ./env.rb ./stations.rb].each(&method(:require))

get('/') { "Goin' beyond Ruby! 🚀" }

def size
  Integer(params[:size] || 3)
end

get '/v1/near' do
  json stations: 
    Stations.near(params[:query], size)
end

get '/v2/near' do
  json stations: 
    Stations.near_par(params[:query], size)
end

get '/v3/near' do
  json stations: 
    Stations.near_duration(params[:query], size)
end

get '/v4/near' do
  json stations: 
    Stations.near_duration_par(params[:query], size)
end

