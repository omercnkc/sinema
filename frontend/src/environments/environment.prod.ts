// Üretim derlemesi (ng build). Yerel Docker yığını da bu derlemeyi kullanır; gateway yine localhost:8080'dedir.
export const environment = {
  production: true,
  apiTabanAdresi: 'http://localhost:8080/api/v1',
  tmdbGorselTabanAdresi: 'https://image.tmdb.org/t/p',
};
