# frozen_string_literal: true
$LOAD_PATH << './stations-rb/'

KIO = Class.new(Proc) do
  attr_accessor :retry
  def initialize(&block) = (super(&block); @retry = 0)
  def retry_n(n) = (self.retry = n; self)
end

Runtime = Struct.new(:program) do
  def execute(state, kio)
    begin
      kio.arity.zero? ? kio.call : kio.call(state)
    rescue => ex
      if kio.retry.nil? || kio.retry <= 1
        raise ex
      else
        kio.retry -= 1
        execute(state, kio)
      end
    end
  end

  def run = (program || []).reduce([], &method(:execute))
end

program = [
  KIO.new { "Oto Brglez" },
  KIO.new(&:upcase),
  KIO.new { _1.reverse },
  KIO.new { _1 + "!!!" },
  KIO.new { raise "Boom! Beer time!!! 🍻" }.retry_n(3),
  KIO.new { |s| puts(s) }
]

Runtime.new(program).run
