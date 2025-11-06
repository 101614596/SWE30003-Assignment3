const API_BASE_URL = 'http://localhost:8080/api';

class API {
    // Products
    static async getAllProducts() {
        const response = await fetch(`${API_BASE_URL}/products`);
        return await response.json();
    }

    static async getProduct(id) {
        const response = await fetch(`${API_BASE_URL}/products/${id}`);
        return await response.json();
    }

    static async searchProducts(category) {
        const response = await fetch(`${API_BASE_URL}/products?category=${category}`);
        return await response.json();
    }

    // Cart
    static async addToCart(productId, quantity) {
        const response = await fetch(`${API_BASE_URL}/cart`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId, quantity })
        });
        return await response.json();
    }

    static async getCart() {
        const response = await fetch(`${API_BASE_URL}/cart`);
        return await response.json();
    }

    static async removeFromCart(productId) {
        const response = await fetch(`${API_BASE_URL}/cart/${productId}`, {
            method: 'DELETE'
        });
        return await response.json();
    }

    // Checkout
    static async checkout(customerData, paymentMethod) {
        const response = await fetch(`${API_BASE_URL}/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customerData, paymentMethod })
        });
        return await response.json();
    }
}