import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FotoService {
  private readonly document = inject(DOCUMENT);
  async reduzir(arquivo: File, tamanho = 400): Promise<string> {
    if (
      !['image/png', 'image/jpeg', 'image/webp'].includes(arquivo.type) ||
      arquivo.size > 5 * 1024 * 1024
    )
      throw new Error('Escolha uma foto PNG, JPEG ou WebP de até 5 MB.');
    const url = await new Promise<string>((resolve, reject) => {
      const leitor = new FileReader();
      leitor.onload = () => resolve(String(leitor.result));
      leitor.onerror = () => reject(new Error('Não foi possível ler a foto.'));
      leitor.readAsDataURL(arquivo);
    });
    return new Promise((resolve, reject) => {
      const imagem = this.document.createElement('img');
      imagem.onload = () => {
        const escala = Math.min(1, tamanho / Math.max(imagem.naturalWidth, imagem.naturalHeight));
        const canvas = this.document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(imagem.naturalWidth * escala));
        canvas.height = Math.max(1, Math.round(imagem.naturalHeight * escala));
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Não foi possível preparar a foto.'));
          return;
        }
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(imagem, 0, 0, canvas.width, canvas.height);
        const foto = canvas.toDataURL('image/jpeg', 0.85);
        if (foto.length > 700000) {
          reject(new Error('A foto ficou muito grande. Escolha outra imagem.'));
          return;
        }
        resolve(foto);
      };
      imagem.onerror = () => reject(new Error('Não foi possível abrir a foto.'));
      imagem.src = url;
    });
  }
}
