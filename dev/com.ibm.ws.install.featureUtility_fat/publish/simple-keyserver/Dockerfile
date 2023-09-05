FROM python:3-alpine

RUN mkdir /public_key

WORKDIR /public_key

RUN printf '\
-----BEGIN PGP PUBLIC KEY BLOCK-----\n\
Comment: Hostname: \n\
Version: Hockeypuck 2.1.0-222-g25248d4\n\
\n\
xjMEY+1Y6RYJKwYBBAHaRw8BAQdAzgcyMvreUdoUXYofjC+mCm1dW9IaPs+nx9rF\n\
dc4mKFLOOARj7VjpEgorBgEEAZdVAQUBAQdA52rgXKV5cdmIqXhIhUvT/Hvktcyy\n\
kKdqt/YJX5GY1CYDAQgHwngEGBYKACACGwwWIQQQyeYjPrW9z8ApyzBx+OYjm2g0\n\
qgUCY+1ZowAKCRBx+OYjm2g0qtuIAQD64SWKyMBtvr8TZT91mIAsMV+npF58B3v0\n\
tNyeDipYJQEA74mtwWh9R7i7+7b3jsxFEMSzu8ZbTk25qSIlQLmE9wM=\n\
=G+gC\n\
-----END PGP PUBLIC KEY BLOCK-----\n\
' > "/public_key/validKey.asc"

EXPOSE 8080



CMD ["python", "-m" , "http.server", "8080"]

# Currently tagged in DockerHub as: jiwoo/simple-keyserver:1.0
