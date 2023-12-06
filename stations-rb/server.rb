# frozen_string_literal: true

$LOAD_PATH << './stations-rb/'
%w[bundler/setup raven json ostruct http sinatra sinatra/json env stations].each(&method(:require))

Raven.configure do |config|
  config.server = ENV.fetch('SENTRY_DSN')
end

use Raven::Rack
set :show_exceptions, :after_handler
set :dump_errors, false
set :raise_errors, false

get('/') { "Goin' beyond Ruby! 🚀" }

def size
  Integer(params[:size] || 3)
end

get '/v1/near' do
  json stations: Stations.near(params[:query], size)
end

get '/v2/near' do
  json stations: Stations.near_par(params[:query], size)
end

get '/v3/near' do
  json stations: Stations.near_duration(params[:query], size)
end

get '/v4/near' do
  json stations: Stations.near_duration_par(params[:query], size)
end

error StationsException do
  json error: 'Sorry there was a nasty search error - ' + env['sinatra.error'].message
end

error do
  json error: 'Sorry there was a nasty error - ' + env['sinatra.error'].message
end
