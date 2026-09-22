import { Routes } from '@angular/router';
import { ArticlieList } from './article-list/article-list';
import { ArticleForm } from './article-form/article-form';

export const ARTICLE_ROUTES: Routes = [
  { path: '', component: ArticlieList },
  { path: 'nouveau', component: ArticleForm },
  { path: ':id/editer', component: ArticleForm },
];