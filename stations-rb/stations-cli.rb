#!/usr/bin/env ruby
# frozen_string_literal: true

$LOAD_PATH << './stations-rb/'
%w[bundler/setup json ostruct optparse pry distance.rb stations.rb env.rb].each(&method(:require))

commands = {
  near: 'Stations near - sorted by geo distance',
  near_par: 'Stations near - sorted by geo distance (parallel)',
  near_duration: 'Stations near location - sorted by walking time',
  near_duration_par: 'Stations near location - sorted by walking time (parallel)'
}.freeze

options = {}
OptionParser.new do |opts|
  opts.banner = 'Usage: stations-cli.rb [options] COMMAND'

  opts.on('-q', '--query QUERY', 'Location query') do |q|
    options[:query] = q
  end
  opts.on('-s', '--size SIZE', 'Number of stations') do |s|
    options[:size] = s
  end
end.parse!

command = ARGV[0]
if command.nil? || !commands.keys.map(&:to_s).include?(command) || options[:query].nil?
  puts 'Error: Both command and name options are required.'
  puts 'Usage: cli.rb COMMAND -n NAME'
  puts "Possible commands are #{commands.keys}"
  exit 1
end

results = Stations.send(command.to_s, options[:query], Integer(options[:size] || 3))
puts results
