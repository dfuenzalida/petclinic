clean:
	rm -rf target
	rm -rf resources/public/css/header.css resources/public/css/petclinic.css resources/public/css/responsive.css resources/public/css/typography.css

run:
	clj -M:dev

repl:
	clj -M:dev:nrepl

test:
	clj -M:test

uberjar:
	clj -T:build all
