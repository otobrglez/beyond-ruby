# frozen_string_literal: true

$LOAD_PATH << './stations-rb/'

def api_key
  ENV["SOME_KEY"]
end

def call_service(api_key)
  puts("Calling #{api_key.downcase.reverse}")
end

# call_service(api_key)
# puts("-----")

def better_api_key
  [ENV["SOME_KEY"]].reject(&:nil?)
end

def better_call_service(api_key)
  puts("Calling better service with SOME_KEY=#{api_key}")
  ["Hello #{api_key}!"]
end

def parse(raw)
  ; raw.map { |r| r.downcase.reverse }
end

program = better_api_key
            .map { |key| better_call_service(key) }
            .flat_map { |r| parse(r) }

# puts(program)

# puts "----- done v2 ----"
# exit(0)

# Better Ruby DYI Monoid (3)
Option = Class.new(Array) do
  def self.from_string(raw)
    raw.nil? ? Option.new([]) : Option.new([raw])
  end

  def zero?
    self.empty?
  end
end

def even_better_api_key
  Option.from_string(ENV["SOME_KEY"])
end

program2 = even_better_api_key
             .map { |key| better_call_service(key) }
             .flat_map { |r| parse(r) }

# puts(program2)

# And beyond
puts("----")

KIO = Class.new(Proc) do
  attr_accessor :retry

  def retry_n(n)
    self.retry = n
    self
  end
end

say_my_name = KIO.new { "Oto Brglez" }
make_it_upper_case = KIO.new { |name| name.upcase }
reverse_it = KIO.new { |name| name.reverse }
print_it = KIO.new { |s| puts(s) }

program3 = [
  say_my_name,
  make_it_upper_case,
  reverse_it,
  KIO.new { |name| "PIV #{name}" },
  reverse_it,
  print_it
]

Runtime = Struct.new(:program) do
  def execute(state, kio)
    begin
      kio.arity.zero? ? kio.call : kio.call(state)
    rescue => ex
      if kio.retry.nil? || kio.retry <= 0
        raise ex
      else
        kio.retry -= 1
        execute(state, kio)
      end
    end
  end

  def run = (program || []).reduce([], &method(:execute))
end

Runtime.new(program3).run
