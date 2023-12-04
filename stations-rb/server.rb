# frozen_string_literal: true
$LOAD_PATH << './stations-rb/'
%w[bundler/setup json ostruct http sinatra sinatra/json env stations].each(&method(:require))
# disable :show_exceptions

# In development you need this:
# set :show_exceptions, :after_handler
set :show_exceptions, false

error StandardError do
  json error: 'Sorry there was a nasty error - ' + env['sinatra.error'].message
end

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

# error RuntimeError do |e|
#  json error: "Failed #{e}"
# end

# error PositionstackException do
#   json stations: []
# end


