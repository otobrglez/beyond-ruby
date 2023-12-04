# The Nix Shell support was added by Oto Brglez.
# If you have additional questions or need support, please reach out.
# - @otobrglez

{ pkgs ? import <nixpkgs> { } }:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = jdk19_headless; };
  ruby = pkgs.ruby.withPackages (ps: with ps; [ nokogiri pry ]);
in


mkShell {
  name = "beyond-ruby";
  buildInputs = [
    jdk19_headless
    sbt
    ruby
  ];

  NIX_ENFORCE_PURITY = 0;
  NIX_SHELL_PRESERVE_PROMPT=1;
  
  shellHook = ''
    set -x
    export JAVA_HOME="${jdk17_headless}"
    echo JAVA_HOME=$JAVA_HOME
    
    bundle config set --local path '.bundle' && \
      bundle install

  '';
}
