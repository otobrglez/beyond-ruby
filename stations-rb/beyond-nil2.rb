# frozen_string_literal: true
$LOAD_PATH << './stations-rb/'

KIO = Class.new(Proc)

my_name = KIO.new { "Oto Brglez" }
to_uppercase = KIO.new(&:upcase)
reverse_it = KIO.new { _1.reverse }
append_mark = KIO.new { _1 + "!!!" }
print = KIO.new { |s| puts(s) }

program = [
  my_name,
  to_uppercase,
  reverse_it,
  reverse_it,
  append_mark,
  print
]

Runtime = Struct.new(:program) do
  def run
    (program || []).reduce([]) do |state, kio|
      kio.arity.zero? ? kio.call : kio.call(state)
    end
  end
end

Runtime.new(program).run
