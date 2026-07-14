.PHONY: test backend frontend install

backend:
	cd backend && mvn spring-boot:run

frontend:
	cd frontend && npm run dev

test:
	cd backend && mvn test
	cd frontend && npm test

install:
	cd frontend && npm install

build:
	cd backend && mvn package -DskipTests
	cd frontend && npm run build
