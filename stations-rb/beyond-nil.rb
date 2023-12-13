# frozen_string_literal: true

$LOAD_PATH << './stations-rb/'

def api_key
  ENV["SOME_KEY"]
end

def call_service(api_key)
  puts("Calling #{api_key.downcase.reverse}")
end

# call_service(api_key)

# Better Ruby (1)
def better_api_key
  [ENV["POSITIONSTACK_API_KEY"]].reject(&:nil?)
end

better_api_key.map { |key| call_service(key) } # => Array...

def better_call_service(api_key)
  puts("Calling better service with #{api_key}")
  ["Hello with API key #{api_key}"]
end

def parse(raw)
  raw.map { |r| r.downcase.reverse }
end

program = better_api_key
            .map { |key| better_call_service(key) }
            .flat_map { |r| parse(r) }

puts(program)

# Better Ruby DYI Monoid (3)
class Option < Array
  def self.from_string(raw)
    raw.nil? ? Option.new([]) : Option.new([raw])
  end

  def zero?
    self.empty?
  end
end

def even_better_api_key
  Option.from_string(ENV["POSITIONSTACK_API_KEY"])
end

program2 = even_better_api_key
             .map { |key| better_call_service(key) }
             .flat_map { |r| parse(r) }
puts(program2)

# And beyond
puts("----")

Effect = Class.new(Proc) do
  attr_accessor :retry

  def retryN(n)
    self.retry = n
    self
  end
end

say_my_name = Effect.new { "Oto Brglez" }
make_it_upper_case = Effect.new { |name| name.upcase }
print_it = Effect.new { |s| puts(s) }

program3 = [
  say_my_name,
  make_it_upper_case,
  Effect.new { |name| name.reverse },
  Effect.new { |name| name.reverse }.retryN(10),
  print_it
]

Runtime = Struct.new(:program) do
  def execute(state, kio)
    begin
      kio.arity.zero? ? kio.call : kio.call(state)
    rescue => ex
      if kio.retry.nil? || kio.retry <= 0
        throw ex
      else
        puts "Crashed with #{ex.message}. Retrying."
        kio.retry -= 1
        execute(state, kio)
      end
    end
  end

  def run
    (program || []).reduce([]) do |state, kio|
      execute(state, kio)
    end
  end
end

Runtime.new(program3).run
